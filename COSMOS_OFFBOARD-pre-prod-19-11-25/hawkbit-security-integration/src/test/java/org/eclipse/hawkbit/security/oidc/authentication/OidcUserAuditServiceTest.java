package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.jpa.UserAuditRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.JpaUserAudit;
import org.eclipse.hawkbit.repository.jpa.model.UserElement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security User Audit")
@Story("Audit user's roles and tenants")
@ExtendWith(MockitoExtension.class)
class OidcUserAuditServiceTest {

    private static final String TEST_TENANT = "TestTenant";
    private static final String TEST_ROLE = "TestRole";

    @Captor
    ArgumentCaptor<Callable<Void>> callableCaptor;
    @Mock
    private UserAuditRepository userAuditRepository;
    @Mock
    private SystemSecurityContext systemSecurityContext;
    @InjectMocks
    private OidcUserAuditService oidcUserAuditService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void updateUserAuthenticationAuditTest() throws Exception {

        // Test User
        JpaUser user = new JpaUser();
        user.setId(1L);

        // Test user role
        JpaRole role = new JpaRole(TEST_ROLE, TEST_ROLE);
        role.setId(1L);
        user.setRoles(Set.of(role));

        // Test user tenant
        UserElement userElement = new UserElement();
        JpaTenantMetaData jpaTenantMetaData = new JpaTenantMetaData(null, TEST_TENANT);
        jpaTenantMetaData.setId(1L);
        userElement.setTenant(jpaTenantMetaData);
        user.setUserElements(List.of(userElement));

        UserTenantAuthorities userTenantAuthorities = UserTenantAuthorities.builder()
                .user(user).build();

        //Test user audit
        List<JpaUserAudit> userAudits = List.of(JpaUserAudit.builder()
                .user(user).role(role).tenant(jpaTenantMetaData).build());
        when(userAuditRepository.findByUser(user)).thenReturn(userAudits);

        when(systemSecurityContext.runAsSystem(callableCaptor.capture())).thenReturn(null);
        oidcUserAuditService.updateUserAuthenticationAudit(userTenantAuthorities);

        Callable<Void> callable = callableCaptor.getValue();
        callable.call();
        verify(userAuditRepository).saveAll(Collections.emptyList());
    }

    @Test
    void userRoleRevokedAuditTest() throws Exception {
        // Test User
        JpaUser user = new JpaUser();
        user.setId(1L);

        UserTenantAuthorities userTenantAuthorities = UserTenantAuthorities.builder()
                .user(user).build();

        //Test user audit
        List<JpaUserAudit> userAudits = List.of(JpaUserAudit.builder().build());
        when(userAuditRepository.findByUser(user)).thenReturn(userAudits);

        when(systemSecurityContext.runAsSystem(callableCaptor.capture())).thenReturn(null);
        oidcUserAuditService.updateUserAuthenticationAudit(userTenantAuthorities);

        Callable<Void> callable = callableCaptor.getValue();
        callable.call();
        verify(userAuditRepository).saveAll(userAudits);
    }

    @Test
    void insertUserAuthenticationAuditTest() throws Exception {
        // Test User
        JpaUser user = new JpaUser();
        user.setId(1L);

        // Test user role
        JpaRole role = new JpaRole(TEST_ROLE, TEST_ROLE);
        role.setId(1L);

        // Test user tenant
        JpaTenantMetaData jpaTenantMetaData = new JpaTenantMetaData(null, TEST_TENANT);
        jpaTenantMetaData.setId(1L);

        //Test user audit
        when(userAuditRepository.findByUserAndRoleAndTenant(user, role, jpaTenantMetaData)).thenReturn(null);

        when(systemSecurityContext.runAsSystem(callableCaptor.capture())).thenReturn(null);
        oidcUserAuditService.insertUserAuthenticationAudit(user, role, jpaTenantMetaData);

        Callable<Void> callable = callableCaptor.getValue();
        callable.call();
        verify(userAuditRepository).save(JpaUserAudit.builder()
                .user(user)
                .role(role)
                .tenant(jpaTenantMetaData)
                .build());
    }

    @Test
    void userAuditAlreadyExistTest() throws Exception {
        // Test User
        JpaUser user = new JpaUser();
        user.setId(1L);

        // Test user role
        JpaRole role = new JpaRole(TEST_ROLE, TEST_ROLE);
        role.setId(1L);

        // Test user tenant
        JpaTenantMetaData jpaTenantMetaData = new JpaTenantMetaData(null, TEST_TENANT);
        jpaTenantMetaData.setId(1L);

        //Test user audit
        when(userAuditRepository.findByUserAndRoleAndTenant(user, role, jpaTenantMetaData))
                .thenReturn(JpaUserAudit.builder().build());

        when(systemSecurityContext.runAsSystem(callableCaptor.capture())).thenReturn(null);
        oidcUserAuditService.insertUserAuthenticationAudit(user, role, jpaTenantMetaData);

        Callable<Void> callable = callableCaptor.getValue();
        callable.call();
        verify(userAuditRepository, times(0)).save(any());
    }

}