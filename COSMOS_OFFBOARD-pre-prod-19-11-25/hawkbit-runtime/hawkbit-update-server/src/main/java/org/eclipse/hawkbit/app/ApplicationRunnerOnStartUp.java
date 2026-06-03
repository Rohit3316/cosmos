package org.eclipse.hawkbit.app;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.hawkbit.app.model.DefaultRoleCreator;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.PermissionRepository;
import org.eclipse.hawkbit.repository.jpa.RoleRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaPermission;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.model.Permission;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import static org.eclipse.hawkbit.im.authentication.SpPermission.APPROVE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.CREATE_REPOSITORY;
import static org.eclipse.hawkbit.im.authentication.SpPermission.CREATE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.CREATE_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.DELETE_REPOSITORY;
import static org.eclipse.hawkbit.im.authentication.SpPermission.DELETE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.DELETE_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.HANDLE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_REPOSITORY;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET_SEC_TOKEN;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_REPOSITORY;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_ROLLOUT;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET;

@Component
public class ApplicationRunnerOnStartUp implements ApplicationRunner {

    /**
     * COSMOS Default Roles
     */
    static final List<DefaultRoleCreator> DEFAULT_ROLES = List.of(

            /* Super Administrator */
            DefaultRoleCreator.builder().name("ADMIN").description("Super Administrator")
                    .hasAllPermissions(true).build(),

            /* Administrator */
            DefaultRoleCreator.builder().name("TENANTADMIN").description("Administrator")
                    .permissions(Set.of(SpPermission.SYSTEM_ADMIN,
                            SpPermission.CREATE_TENANT,
                            SpPermission.READ_ALL_TENANT,
                            SpPermission.UPDATE_TENANT,
                            SpPermission.DELETE_TENANT)).excludePermissions(true).build(),

            /* Content Manager */
            DefaultRoleCreator.builder().name("CONTENTMANAGER").description("Content Manager")
                    .permissions(Set.of(READ_TARGET, READ_TARGET_SEC_TOKEN, READ_REPOSITORY, UPDATE_REPOSITORY,
                            CREATE_REPOSITORY, DELETE_REPOSITORY, DOWNLOAD_REPOSITORY_ARTIFACT, READ_ROLLOUT)).build(),

            /* Device Manager */
            DefaultRoleCreator.builder().name("DEVICEMANAGER").description("Device Manager")
                    .permissions(Set.of(READ_TARGET, READ_TARGET_SEC_TOKEN, UPDATE_TARGET, CREATE_TARGET,
                            DELETE_TARGET, READ_REPOSITORY, DOWNLOAD_REPOSITORY_ARTIFACT, READ_ROLLOUT)).build(),

            /* Roll-out Manager */
            DefaultRoleCreator.builder().name("ROLLOUTMANAGER").description("Roll-out Manager")
                    .permissions(Set.of(READ_TARGET, READ_TARGET_SEC_TOKEN, READ_REPOSITORY,
                            DOWNLOAD_REPOSITORY_ARTIFACT, READ_ROLLOUT, CREATE_ROLLOUT, UPDATE_ROLLOUT,
                            DELETE_ROLLOUT, HANDLE_ROLLOUT, APPROVE_ROLLOUT)).build(),

            /* Viewer */
            DefaultRoleCreator.builder().name("VIEWER").description("Viewer")
                    .permissions(Set.of(READ_TARGET, READ_TARGET_SEC_TOKEN, READ_REPOSITORY,
                            DOWNLOAD_REPOSITORY_ARTIFACT, READ_ROLLOUT)).build(),

            /* M2M Role */
            DefaultRoleCreator.builder().name(SpPermission.SpringEvalExpressions.API_ROLE).description("M2M Role")
                    .hasAllPermissions(true).build()
    );
    static final List<String> DEFAULT_TENANTS = Collections.singletonList("DEFAULT");
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunnerOnStartUp.class);
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private SystemManagement systemManagement;
    @Autowired
    private SystemSecurityContext systemSecurityContext;

    /**
     * Overriding {@link ApplicationRunner#run(ApplicationArguments)}
     *
     * @param args arguments
     * @throws Exception exceptions
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        systemSecurityContext.runAsSystem(() -> {
            /* initialize permissions */
            initPermissions();

            /* initialize roles */
            initSeedRoles();

            /* initialize tenants */
            initSeedTenants();

            return null;
        });
    }
    //Rohit Salunkhe
//     @Override
// public void run(ApplicationArguments args) throws Exception {
//     systemSecurityContext.runAsSystem(() -> {

//         // 1 create default tenant first
//         initSeedTenants();

//         // 2 then permissions
//         initPermissions();

//         // 3 then roles
//         initSeedRoles();

//         return null;
//     });
// }
//Rohit Salunkhe

    /**
     * Get all permissions from DB.
     *
     * @return set of Permission
     */
    private Set<Permission> getAllPermissions() {
        return new HashSet<>(permissionRepository.findAll());
    }

    /**
     * Get the set of permissions from DB, that are included in role's permissions.
     *
     * @param permissions set of permissions that are included in a role.
     * @return set of Permission
     */
    private Set<Permission> includePermissions(Set<String> permissions) {
        return permissions != null && !permissions.isEmpty()
                ? new HashSet<>(permissionRepository.findAllByNames(permissions))
                : new HashSet<>();
    }

    /**
     * Get the set of permissions from DB, where excluding permissions that are given in the permissions' collection.
     *
     * @param permissions set of permissions that are excluded from a role.
     * @return set of Permission
     */
    private Set<Permission> excludePermissions(Set<String> permissions) {
        return permissions != null && !permissions.isEmpty()
                ? getAllPermissions().stream()
                .filter(p -> !permissions.contains(p.getName()))
                .collect(Collectors.toSet())
                : new HashSet<>();
    }

    /**
     * Method to insert permissions from SpPermissions class into DB
     */
    private void initPermissions() {
        List<String> permissions = SpPermission.getAllAuthorities();
        if (!permissions.isEmpty()) {
            List<JpaPermission> createPermissions = new ArrayList<>();
            permissions.forEach(p -> {
                if (permissionRepository.findByName(p).isEmpty()) {
                    createPermissions.add(new JpaPermission(p, p + " permission"));
                }
            });
            if (!createPermissions.isEmpty()) {
                permissionRepository.saveAll(createPermissions);
            }
        }
    }

    /**
     * Method to insert default roles into DB
     */
    private void initSeedRoles() {
        if (!DEFAULT_ROLES.isEmpty()) {
            List<JpaRole> createRoles = new ArrayList<>();
            DEFAULT_ROLES.forEach(r -> {
                JpaRole jpaRole = (JpaRole) roleRepository.findByName(r.getName())
                        .orElse(new JpaRole(r.getName(), r.getDescription()));
                updateRolePermissions(jpaRole, r);
                createRoles.add(jpaRole);
            });
            if (!createRoles.isEmpty()) {
                roleRepository.saveAll(createRoles);
            }
        }
    }

    private JpaRole updateRolePermissions(JpaRole jpaRole, DefaultRoleCreator roleCreator) {
        if (roleCreator.isHasAllPermissions()) {
            jpaRole.setPermissions(getAllPermissions());
        } else if (roleCreator.isExcludePermissions()) {
            jpaRole.setPermissions(excludePermissions(roleCreator.getPermissions()));
        } else {
            jpaRole.setPermissions(includePermissions(roleCreator.getPermissions()));
        }
        return jpaRole;
    }

    /**
     * Method to insert default tenants into DB
     */
    private void initSeedTenants() {
        if (!DEFAULT_TENANTS.isEmpty()) {
            DEFAULT_TENANTS.forEach(tenant -> {
                try {
                    systemManagement.createTenantWithoutPermission(tenant);
                } catch (Exception e) {
                    LOGGER.info("Exception: {}", e.getMessage());
                }
            });
        }
    }
}
