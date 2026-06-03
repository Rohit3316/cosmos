# Quick Reference: Security & Authentication File Index

## By Category

### 1. COSMOS Management API
```
cosmos-mgmt-api/src/main/java/com/stellantis/cosmos/mgmt/app/Start.java
cosmos-mgmt-api/src/main/resources/
  - application-int.properties
  - application-local.properties
  - application-qa.properties
  - application.properties
```

### 2. Core Security Framework (hawkbit-security-core)
```
hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/
  - DdiSecurityProperties.java
  - HawkbitSecurityProperties.java
  - InMemoryUserAuthoritiesResolver.java
  - OidcSecurityProperties.java
  - SecurityConstants.java
  - SecurityContextTenantAware.java
  - SecurityTokenGenerator.java
  - SpringSecurityAuditorAware.java
  - SystemSecurityContext.java

hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/
  - PermissionService.java
  - TenantAwareAuthenticationDetails.java
  - TenantUserPasswordAuthenticationToken.java
  - UserPrincipal.java
```

### 3. Security Integration & Filters (hawkbit-security-integration)

#### Pre-Authentication Filters
```
hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/
  - AbstractControllerAuthenticationFilter.java
  - ControllerPreAuthenticateSecurityTokenFilter.java
  - ControllerPreAuthenticatedAnonymousDownload.java
  - ControllerPreAuthenticatedAnonymousFilter.java
  - ControllerPreAuthenticatedGatewaySecurityTokenFilter.java
  - ControllerPreAuthenticatedSecurityHeaderFilter.java
  - DmfTenantSecurityToken.java
  - HeaderAuthentication.java
  - PreAuthenticationFilter.java
  - PreAuthTokenSourceTrustAuthenticationProvider.java
  - TenantAwareWebAuthenticationDetails.java
```

#### OIDC Authentication
```
hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/
  
  Core:
  - JwtAuthoritiesExtractor.java
  - JwtAuthoritiesValidator.java
  - JwtAuthoritiesOidcUserService.java
  - OidcBearerTokenAuthenticationFilter.java
  - OidcRestAuthenticationEntryPoint.java
  
  Success/Error Handling:
  - OidcAuthenticationSuccessHandler.java
  - OidcLogoutHandler.java
  - OidcLogoutSuccessHandler.java
  
  Audit:
  - OidcUserAuditService.java
  
  Utilities:
  - JwtUtil.java
  
  Models:
  - model/UserTenantAuthorities.java
  - model/UserTenantRole.java
  
  Exceptions:
  - exception/OAuth2AccessDeniedException.java
  - exception/ExceptionInfo.java
```

### 4. HTTP-Level Security (hawkbit-http-security)
```
hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/
  - AbstractHttpControllerAuthenticationFilter.java
  - ControllerTenantAwareAuthenticationDetailsSource.java
  - DosFilter.java
  - HttpControllerPreAuthenticateAnonymousDownloadFilter.java
  - HttpControllerPreAuthenticateSecurityTokenFilter.java
  - HttpControllerPreAuthenticatedGatewaySecurityTokenFilter.java
  - HttpControllerPreAuthenticatedSecurityHeaderFilter.java
  - HttpDownloadAuthenticationFilter.java
```

### 5. Security Auto-Configuration (hawkbit-autoconfigure)
```
hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/
  - EnableHawkbitManagedSecurityConfiguration.java
  - InMemoryUserManagementAutoConfiguration.java
  - MultiUserProperties.java
  - OidcUserManagementAutoConfiguration.java
  - SecurityAutoConfiguration.java
  - SecurityManagedConfiguration.java
```

---

## By Authentication Method

### OAuth2/OIDC (JWT-Based)
```
- JwtAuthoritiesExtractor.java
- JwtAuthoritiesValidator.java
- JwtAuthoritiesOidcUserService.java
- OidcBearerTokenAuthenticationFilter.java
- OidcAuthenticationSuccessHandler.java
- OidcLogoutHandler.java
- OidcLogoutSuccessHandler.java
- OidcRestAuthenticationEntryPoint.java
- JwtUtil.java
- OidcUserAuditService.java
- OidcSecurityProperties.java
- OidcUserManagementAutoConfiguration.java
- UserTenantAuthorities.java
- UserTenantRole.java
```

### Token-Based Authentication
```
- AbstractControllerAuthenticationFilter.java
- ControllerPreAuthenticateSecurityTokenFilter.java
- DmfTenantSecurityToken.java
- PreAuthTokenSourceTrustAuthenticationProvider.java
- HttpControllerPreAuthenticateSecurityTokenFilter.java
- SecurityTokenGenerator.java
- DdiSecurityProperties.java
```

### Header-Based Authentication
```
- ControllerPreAuthenticatedSecurityHeaderFilter.java
- HeaderAuthentication.java
- HttpControllerPreAuthenticatedSecurityHeaderFilter.java
- ControllerTenantAwareAuthenticationDetailsSource.java
```

### Anonymous Access (Controlled)
```
- ControllerPreAuthenticatedAnonymousDownload.java
- ControllerPreAuthenticatedAnonymousFilter.java
- HttpControllerPreAuthenticateAnonymousDownloadFilter.java
```

### Gateway Token Authentication
```
- ControllerPreAuthenticatedGatewaySecurityTokenFilter.java
- HttpControllerPreAuthenticatedGatewaySecurityTokenFilter.java
```

---

## By Functional Area

### Authentication & User Management
```
- UserPrincipal.java
- PermissionService.java
- InMemoryUserAuthoritiesResolver.java
- SpringSecurityAuditorAware.java
- InMemoryUserManagementAutoConfiguration.java
```

### Multi-Tenancy Support
```
- SecurityContextTenantAware.java
- TenantUserPasswordAuthenticationToken.java
- TenantAwareAuthenticationDetails.java
- TenantAwareWebAuthenticationDetailsSource.java
- TenantAwareWebAuthenticationDetails.java
- UserTenantAuthorities.java
- UserTenantRole.java
```

### Security Configuration & Properties
```
- HawkbitSecurityProperties.java
- DdiSecurityProperties.java
- OidcSecurityProperties.java
- SecurityConstants.java
- MultiUserProperties.java
- SecurityAutoConfiguration.java
- SecurityManagedConfiguration.java
```

### HTTP Request Processing
```
- AbstractHttpControllerAuthenticationFilter.java
- DosFilter.java
- HttpDownloadAuthenticationFilter.java
- All Http*Filter classes
```

### Error Handling & Audit
```
- OidcRestAuthenticationEntryPoint.java
- OidcUserAuditService.java
- OAuth2AccessDeniedException.java
- ExceptionInfo.java
```

---

## Configuration Properties by Environment

### Local Development (application-local.properties)
```
spring.security.user.name=admin
spring.security.user.password={noop}admin

hawkbit.server.ddi.security.authentication.anonymous.enabled=true
hawkbit.server.ddi.security.authentication.targettoken.enabled=true
hawkbit.server.ddi.security.authentication.gatewaytoken.enabled=true

OIDC Configuration:
- Issuer: https://idfed-preprod.mpsa.com:443
- Client ID: MUWABNBDNBCKBAXEQPYRQNFCECFPKVWD
- Scopes: openid,prd:aug,profile
```

### Integration/Production (application-int.properties, application-qa.properties)
```
spring.security.user.name=admin

hawkbit.server.ddi.security.authentication.anonymous.enabled=false
hawkbit.server.ddi.security.authentication.targettoken.enabled=true
hawkbit.server.ddi.security.authentication.gatewaytoken.enabled=false

DOS Protection:
- maxArtifactSize=1073741824 (1GB)
- maxArtifactStorage=107374182400 (100GB)
```

---

## File Statistics

| Category | Count |
|----------|-------|
| Core Security Classes | 9 |
| Identity Management Classes | 4 |
| Integration Filters | 11 |
| OIDC Components | 10 |
| HTTP Filters | 8 |
| Autoconfiguration Classes | 6 |
| Configuration Files | 4+ |
| **Total** | **113+** |

---

## Dependency Relationships

```
Start.java
    ↓
SecurityAutoConfiguration (beans)
    ↓
SecurityManagedConfiguration (@EnableWebSecurity)
    ↓
{
  - InMemoryUserManagementAutoConfiguration
  - OidcUserManagementAutoConfiguration
}
    ↓
{
  - HTTP Filters (DosFilter, Http*Filter)
  - Security Integration Filters (Controller*Filter)
  - OIDC Authentication (OidcBearerTokenAuthenticationFilter)
}
    ↓
Core Security (HawkbitSecurityProperties, SecurityTokenGenerator, etc.)
```

---

## Key Implementation Notes

### Authentication Flow Priority
1. **DoS Check** → DosFilter
2. **Pre-Authentication Extraction** → Http*Filter or Controller*Filter
3. **Token/Header Validation** → PreAuthTokenSourceTrustAuthenticationProvider
4. **JWT/OIDC Validation** → OidcBearerTokenAuthenticationFilter, JwtAuthoritiesValidator
5. **Tenant Context Loading** → TenantAwareAuthenticationDetailsSource
6. **Authority Resolution** → JwtAuthoritiesExtractor or InMemoryUserAuthoritiesResolver
7. **Audit Logging** → SpringSecurityAuditorAware or OidcUserAuditService

### Tenant Isolation
- All authentication tokens/principals include tenant context
- Multi-level tenant awareness: properties, tokens, details, authorities
- Per-tenant role/permission mapping

### Security Properties Hierarchy
```
@ConfigurationProperties("hawkbit.server.ddi.security.*")
                                ↓
                        DdiSecurityProperties
                                ↓
            Used by DdiApiAutoConfiguration
                                ↓
            Affects Controller*Filter behavior
```
