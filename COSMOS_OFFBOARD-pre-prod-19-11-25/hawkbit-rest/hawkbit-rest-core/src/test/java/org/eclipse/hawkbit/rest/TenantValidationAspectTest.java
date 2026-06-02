package org.eclipse.hawkbit.rest;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.Optional;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.rest.aspect.TenantValidationAspect;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.ALL_TENANTS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - TenantAspect")
@Story("Tenant Aware AOP Test")
@ExtendWith(MockitoExtension.class)
public class TenantValidationAspectTest {

    private static final String REQUEST_TENANT = "requestTenant";
    private static final String LOGGED_TENANT = "loggedTenant";
    private static final String TENANTS_1_TARGET_ID_6_URL = "/management/v1/tenants/1/targetId/6";
    private static final String PROCEED = "proceed";
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private SystemManagement systemManagement;
    @Mock
    private SystemSecurityContext systemSecurityContext;
    @Mock
    private AbstractAuthenticationToken token;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @InjectMocks
    private TenantValidationAspect tenantValidationAspect;
    @Captor
    private ArgumentCaptor<TenantAwareAuthenticationDetails> tenantAwareAuthenticationDetailsArgumentCaptor;

    @AfterAll
    public static void tearDownStaticMock() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    public void setUpStaticMock() {
        // Mock request and request attributes
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @Test
    @Description("Verifies that the tenant validation aspect is working as expected.")
    void givenValidTenantIdWhenValidateTenantIdThenSuccess() throws Throwable {
        final String requestTenant = REQUEST_TENANT;
        final String loggedTenant = LOGGED_TENANT;

        // Logged security context
        when(token.getDetails()).thenReturn(new TenantAwareAuthenticationDetails(loggedTenant, false));
        SecurityContextHolder.setContext(new SecurityContextImpl(token));

        TenantMetaData tenantMetaData = new JpaTenantMetaData(new JpaDistributionSetType(), requestTenant);
        when(systemSecurityContext.getUserAuthorizedTenants()).thenReturn(Set.of(requestTenant));
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);

        when(httpServletRequest.getRequestURI()).thenReturn(TENANTS_1_TARGET_ID_6_URL);
        when(joinPoint.proceed(joinPoint.getArgs())).thenReturn(PROCEED);

        Object result = tenantValidationAspect.validateTenantId(joinPoint);
        Assertions.assertEquals(PROCEED, result);
        verify(token, times(1)).setDetails(tenantAwareAuthenticationDetailsArgumentCaptor.capture());
        Assertions.assertEquals(requestTenant, tenantAwareAuthenticationDetailsArgumentCaptor.getValue().getTenant());
    }

    @Test
    @Description("Verifies that the tenant validation aspect throws EntityNotFoundException on invalid tenantId.")
    void givenInValidTenantIdWhenValidateTenantIdThenFailure() throws Throwable {

        when(httpServletRequest.getRequestURI()).thenReturn(TENANTS_1_TARGET_ID_6_URL);
        when(systemManagement.getTenantMetadataNoPermission(1L))
                .thenThrow(new EntityNotFoundException("Invalid Tenant: Tenant not found"));
        assertThrows(EntityNotFoundException.class, () -> tenantValidationAspect.validateTenantId(joinPoint));
        verify(joinPoint, never()).proceed(any());
    }

    @Test
    @Description("Verifies that the tenant validation aspect throws IllegalArgumentException on invalid Url.")
    void givenUrlWithoutTenantIdWhenValidateTenantIdThenFailure() throws Throwable {
        when(httpServletRequest.getRequestURI()).thenReturn("/management/v1/targetId/6");
        assertThrows(IllegalArgumentException.class, () -> tenantValidationAspect.validateTenantId(joinPoint));
        verify(joinPoint, never()).proceed(any());
    }

    @Test
    @Description("Verifies that the tenant validation aspect throws OAuth2AuthorizationException on unauthorized tenantId.")
    void givenUnauthorizedTenantIdWhenValidateTenantIdThenFailure() throws Throwable {
        final String requestTenant = REQUEST_TENANT;
        final String loggedTenant = LOGGED_TENANT;

        TenantMetaData tenantMetaData = new JpaTenantMetaData(new JpaDistributionSetType(), requestTenant);
        when(systemSecurityContext.getUserAuthorizedTenants()).thenReturn(Set.of(loggedTenant));
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);

        when(httpServletRequest.getRequestURI()).thenReturn(TENANTS_1_TARGET_ID_6_URL);
        assertThrows(OAuth2AuthorizationException.class, () -> tenantValidationAspect.validateTenantId(joinPoint));
        verify(joinPoint, never()).proceed(any());
    }

    @Test
    @Description("Verifies that the tenant validation aspect will be success on M2M all tenants authorization.")
    void givenM2MUserWhenValidateTenantIdThenSuccess() throws Throwable {
        final String requestTenant = REQUEST_TENANT;
        final String loggedTenant = LOGGED_TENANT;

        // Logged security context
        when(token.getDetails()).thenReturn(new TenantAwareAuthenticationDetails(loggedTenant, false));
        SecurityContextHolder.setContext(new SecurityContextImpl(token));

        TenantMetaData tenantMetaData = new JpaTenantMetaData(new JpaDistributionSetType(), requestTenant);
        when(systemSecurityContext.getUserAuthorizedTenants()).thenReturn(Set.of(loggedTenant, ALL_TENANTS));
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);

        when(httpServletRequest.getRequestURI()).thenReturn(TENANTS_1_TARGET_ID_6_URL);
        when(joinPoint.proceed(joinPoint.getArgs())).thenReturn(PROCEED);

        Object result = tenantValidationAspect.validateTenantId(joinPoint);
        Assertions.assertEquals(PROCEED, result);
        verify(token, times(1)).setDetails(tenantAwareAuthenticationDetailsArgumentCaptor.capture());
        Assertions.assertEquals(requestTenant, tenantAwareAuthenticationDetailsArgumentCaptor.getValue().getTenant());
    }

    @Test
    @Description("verifies that the tenant validation aspect never switch tenant if the logged tenant is same as request tenant.")
    void givenLoggedTenantIdWhenValidateTenantIdThenNeverSwitchTenant() throws Throwable {
        final String requestTenant = LOGGED_TENANT;
        final String loggedTenant = LOGGED_TENANT;

        // Logged security context
        when(token.getDetails()).thenReturn(new TenantAwareAuthenticationDetails(loggedTenant, false));
        SecurityContextHolder.setContext(new SecurityContextImpl(token));

        TenantMetaData tenantMetaData = new JpaTenantMetaData(new JpaDistributionSetType(), requestTenant);
        when(systemSecurityContext.getUserAuthorizedTenants()).thenReturn(Set.of(loggedTenant));
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);

        when(httpServletRequest.getRequestURI()).thenReturn(TENANTS_1_TARGET_ID_6_URL);
        when(joinPoint.proceed(joinPoint.getArgs())).thenReturn(PROCEED);

        Object result = tenantValidationAspect.validateTenantId(joinPoint);
        Assertions.assertEquals(PROCEED, result);
        verify(token, never()).setDetails(any());
    }

    @Test
    @Description("Verifies that the vehicle tenant validation aspect is working as expected.")
    void givenValidControllerIdWhenValidateAndSetTenantThenSuccess() throws Throwable {
        final String controllerId = "controller123";
        final String tenantName = "vehicleTenant";

        lenient().when(token.getDetails()).thenReturn(new TenantAwareAuthenticationDetails(LOGGED_TENANT, false));
        SecurityContextHolder.setContext(new SecurityContextImpl(token));

        when(systemManagement.getTenantByControllerId(controllerId)).thenReturn(Optional.of(tenantName));
        lenient().when(httpServletRequest.getRequestURI()).thenReturn("/management/v1/controllers/" + controllerId + "/someOperation");

        Object[] expectedArgs = {null, controllerId}; // Adjust args if necessary
        when(joinPoint.getArgs()).thenReturn(expectedArgs);
        when(joinPoint.proceed(expectedArgs)).thenReturn(PROCEED);

        Object result = tenantValidationAspect.validateAndSetTenant(joinPoint);
        Assertions.assertEquals(PROCEED, result);
        verify(systemManagement, times(1)).getTenantByControllerId(controllerId);
        verify(joinPoint, times(1)).proceed(expectedArgs);
    }



    @Test
    @Description("Verifies that an exception is thrown when the controller ID is invalid and tenant validation fails.")
    void givenInvalidControllerIdWhenValidateAndSetTenantThenThrowException() throws Throwable {
        final String controllerId = "invalidController";
        when(httpServletRequest.getRequestURI()).thenReturn("/management/v1/controllers/" + controllerId + "/someOperation");
        when(systemManagement.getTenantByControllerId(controllerId)).thenReturn(Optional.empty());
        // Change to EntityNotFoundException
        EntityNotFoundException exception = Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> tenantValidationAspect.validateAndSetTenant(joinPoint)
        );
        Assertions.assertEquals("Controller ID not found: " + controllerId, exception.getMessage());
        verify(token, never()).setDetails(any());
    }

}