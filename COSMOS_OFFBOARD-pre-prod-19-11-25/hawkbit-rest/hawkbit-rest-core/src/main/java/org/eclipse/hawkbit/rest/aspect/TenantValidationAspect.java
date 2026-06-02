package org.eclipse.hawkbit.rest.aspect;


import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.ALL_TENANTS;

@Aspect
@Description("Handles non-existing entities by throwing EntityNotFoundException.")
public class TenantValidationAspect {

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    /**
     * Around advice that intercepts methods annotated with @TenantAware to validate the tenant ID.
     *
     * @param joinPoint the join point representing the intercepted method
     * @return the result of the intercepted method
     * @throws Throwable if an error occurs during method execution
     */
    @Around("@annotation(TenantAware)")
    public Object validateTenantId(ProceedingJoinPoint joinPoint) throws Throwable {
        final String requestTenant = getRequestedTenant(RequestContextHolder.getRequestAttributes());
        validateWithUserAuthorizedTenants(requestTenant);
        return runInRequestedTenant(requestTenant, joinPoint);
    }

    /**
     * Extracts the tenant ID from the request attributes and validate it within COSMOS authorized tenants.
     *
     * @param requestAttributes the (@link RequestAttributes) to extract the tenant ID from
     * @return the tenant
     */
    private String getRequestedTenant(RequestAttributes requestAttributes) {
        return (requestAttributes instanceof ServletRequestAttributes attributes) ? systemManagement
                .getTenantMetadataNoPermission(extractTenantIdFromRequest(attributes.getRequest())).getTenant() : null;
    }

    /**
     * Validates the tenant ID against the user's authorized tenants.
     *
     * @param tenant the tenant to validate
     */
    private void validateWithUserAuthorizedTenants(final String tenant) {
        Set<String> authorizedTenants = systemSecurityContext.getUserAuthorizedTenants();
        if (!authorizedTenants.contains(tenant) && !authorizedTenants.contains(ALL_TENANTS)) {
            throw new OAuth2AuthorizationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED), "User not authorized on tenant");
        }
    }

    /**
     * Extracts the tenant ID from the request URL.
     *
     * @param request the current HTTP request
     * @return the tenant ID
     * @throws IllegalArgumentException if the tenant ID is not found in the URL
     */
    private long extractTenantIdFromRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String[] parts = requestURI.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("tenants".equals(parts[i]) && i + 1 < parts.length) {
                return Long.parseLong(parts[i + 1]);
            }
        }
        throw new IllegalArgumentException("Invalid Tenant: Tenant not found");
    }

    /**
     * Method to execute requested join point in given tenant.
     * Before execution of the join point, the requested tenant is replaced in the security content.
     * After execution, its restore back to old context tenant.
     *
     * @param tenant    the requested tenant
     * @param joinPoint the join point representing the intercepted method
     * @return Object the method execution result.
     * @throws Throwable if an error occurs during method execution
     */
    private Object runInRequestedTenant(final String tenant, final ProceedingJoinPoint joinPoint) throws Throwable {
        String effectiveTenant = tenant;
        if (StringUtils.hasText(effectiveTenant) && SecurityContextHolder.getContext().getAuthentication() instanceof final AbstractAuthenticationToken token
                && token.getDetails() instanceof final TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails) {
            String oldTenant = tenantAwareAuthenticationDetails.getTenant();
            if (!tenant.equals(oldTenant)) {
                token.setDetails(new TenantAwareAuthenticationDetails(effectiveTenant, tenantAwareAuthenticationDetails.isController()));
            }
        }
            return joinPoint.proceed(joinPoint.getArgs());
    }

    /**
     * Around advice that intercepts methods annotated with @VehicleTenantAware to validate the controllerId and set the tenant context.
     *
     * @param joinPoint the join point representing the intercepted method
     * @return the result of the intercepted method
     * @throws Throwable if an error occurs during method execution
     */
    @Around("@annotation(VehicleTenantAware)")
    public Object validateAndSetTenant(ProceedingJoinPoint joinPoint) throws Throwable {
        final String controllerId = getControllerIdFromRequest(RequestContextHolder.getRequestAttributes());
        String tenantName = systemManagement.getTenantByControllerId(controllerId)
                .orElseThrow(() -> new EntityNotFoundException("Controller ID not found: " + controllerId));
        return runInRequestedTenant(tenantName, joinPoint);
    }

    /**
     * Extracts the controllerId from the request URL.
     *
     * @param requestAttributes the request attributes containing the request
     * @return the controllerId
     * @throws ResponseStatusException if the controllerId is not found
     */
    private String getControllerIdFromRequest(RequestAttributes requestAttributes) {
        if (requestAttributes instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String requestURI = request.getRequestURI();
            String[] parts = requestURI.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("controllers".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1]; // Extracting controllerId from the URL
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Controller ID: Controller not found in request");
    }

}

