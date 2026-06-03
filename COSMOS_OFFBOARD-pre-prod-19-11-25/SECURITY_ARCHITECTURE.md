# Security Architecture Summary

## High-Level Security Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     COSMOS Management API (Start.java)                  │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────────────┐
│         Spring Security Framework (@EnableWebSecurity)                  │
│         (SecurityManagedConfiguration)                                  │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┬─────────────────┐
        │                  │                  │                 │
        ▼                  ▼                  ▼                 ▼
   ┌─────────┐      ┌──────────┐      ┌────────────┐    ┌──────────────┐
   │  REST   │      │Management│      │ DDI/Device │    │ OAuth2/OIDC  │
   │Security │      │API Config│      │   Config   │    │   Config     │
   └─────────┘      └──────────┘      └────────────┘    └──────────────┘
        │                  │                  │                 │
        └──────────────────┼──────────────────┴─────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │           HTTP Filter Chain                         │
        │  (Processes incoming requests)                      │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  1. DosFilter (DoS Protection)                      │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  2. Pre-Authentication Extraction Layer             │
        │     ├─ HttpControllerPreAuthenticateSecurityToken   │
        │     ├─ HttpControllerPreAuthenticatedGatewayToken   │
        │     ├─ HttpControllerPreAuthenticatedSecurityHeader │
        │     └─ HttpControllerPreAuthenticateAnonymousDown   │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  3. Token Validation & Authentication               │
        │     ├─ PreAuthTokenSourceTrustAuthenticationProvider│
        │     ├─ OidcBearerTokenAuthenticationFilter (JWT)    │
        │     └─ JwtAuthoritiesValidator                      │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  4. Tenant Context Loading                          │
        │     ├─ ControllerTenantAwareAuthenticationDetails   │
        │     └─ TenantAwareWebAuthenticationDetails          │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  5. Authority Resolution                            │
        │     ├─ JwtAuthoritiesExtractor                      │
        │     ├─ InMemoryUserAuthoritiesResolver              │
        │     └─ JwtAuthoritiesOidcUserService                │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  6. Audit & Context Setup                           │
        │     ├─ SpringSecurityAuditorAware                   │
        │     ├─ OidcUserAuditService                         │
        │     └─ SecurityContextTenantAware                   │
        └──────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────────┐
        │  7. Business Logic (with authenticated context)     │
        │     ├─ PermissionService (authorization checks)     │
        │     └─ Controller Handlers                          │
        └──────────────────────────────────────────────────────┘
```

---

## Authentication Methods Supported

### 1. Token-Based Authentication Flow
```
Client Request
    │
    ├─ Header: X-Security-Token: <token>
    │
    ▼
HttpControllerPreAuthenticateSecurityTokenFilter
    │
    ├─ Extract token from header
    │
    ▼
PreAuthTokenSourceTrustAuthenticationProvider
    │
    ├─ Validate token source
    │
    ▼
Create PreAuthenticatedAuthenticationToken
    │
    ├─ User Principal created
    │
    ▼
Request Processed with Authentication
```

### 2. OAuth2/OIDC JWT Authentication Flow
```
Client Request
    │
    ├─ Header: Authorization: Bearer <jwt_token>
    │
    ▼
OidcBearerTokenAuthenticationFilter
    │
    ├─ Extract JWT from Bearer token
    │
    ▼
JwtUtil.parse() + JwtAuthoritiesValidator
    │
    ├─ Validate JWT signature and expiry
    ├─ Validate token authorities
    │
    ▼
JwtAuthoritiesExtractor
    │
    ├─ Extract user roles/authorities from JWT claims
    │
    ▼
JwtAuthoritiesOidcUserService
    │
    ├─ Load user details from OIDC provider
    │
    ▼
Create OAuth2AuthenticatedPrincipal
    │
    ├─ User identity with authorities
    │
    ▼
Request Processed with OAuth2 Authentication
```

### 3. Gateway Token Authentication Flow
```
Gateway Request
    │
    ├─ Pre-authenticated token from gateway
    │
    ▼
HttpControllerPreAuthenticatedGatewaySecurityTokenFilter
    │
    ├─ Trust gateway source
    ├─ Extract token
    │
    ▼
PreAuthTokenSourceTrustAuthenticationProvider (trust source)
    │
    ├─ Skip validation (source trusted)
    │
    ▼
Create PreAuthenticatedAuthenticationToken
    │
    ▼
Request Processed
```

### 4. Header-Based Authentication Flow
```
Client Request
    │
    ├─ Custom Security Headers
    │
    ▼
HttpControllerPreAuthenticatedSecurityHeaderFilter
    │
    ├─ Extract authentication from headers
    ├─ Parse HeaderAuthentication
    │
    ▼
ControllerTenantAwareAuthenticationDetailsSource
    │
    ├─ Extract tenant from header
    ├─ Create TenantAwareAuthenticationDetails
    │
    ▼
Create Authentication with Tenant Context
    │
    ▼
Request Processed with Tenant Awareness
```

---

## Multi-Tenant Security Context

```
┌─────────────────────────────────────────────────────────────┐
│              User Authentication                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────▼──────────────────┐
        │    Token/Request Contains       │
        │    Tenant Identifier            │
        └──────────────┬──────────────────┘
                       │
    ┌──────────────────▼──────────────────────────────────┐
    │ TenantAwareAuthenticationDetailsSource               │
    │    ├─ Extract tenant ID from token/header           │
    │    └─ Create TenantAwareAuthenticationDetails        │
    └──────────────────┬──────────────────────────────────┘
                       │
    ┌──────────────────▼──────────────────────────────────┐
    │ UserTenantAuthorities (Per-Tenant Mapping)           │
    │    ├─ Tenant ID → Authorities mapping                │
    │    └─ UserTenantRole[] array                         │
    └──────────────────┬──────────────────────────────────┘
                       │
    ┌──────────────────▼──────────────────────────────────┐
    │ SecurityContextTenantAware                           │
    │    ├─ Store tenant context in SecurityContext        │
    │    ├─ Available to all business logic                │
    │    └─ Automatic tenant filtering applied             │
    └──────────────────────────────────────────────────────┘

Result: All operations automatically scoped to tenant
```

---

## Configuration Classes & Their Roles

### SecurityManagedConfiguration (@EnableWebSecurity)
```
Dynamic nested configurations based on properties:

If REST API enabled:
    ├─ HttpSecurityConfiguration (REST endpoints)
    │   └─ Configures SecurityFilterChain for /api/**
    │
If Management API enabled:
    ├─ HttpMgmtSecurityConfiguration (@EnableWebSecurity)
    │   └─ Configures SecurityFilterChain for /mgmt/**
    │
If DDI/Device API enabled:
    ├─ HttpControllerSecurityConfiguration (@EnableWebSecurity)
    │   └─ Configures SecurityFilterChain for /controller/**
```

### InMemoryUserManagementAutoConfiguration
```
Activated: When spring.security.user.* properties defined

Provides:
    ├─ InMemoryUserDetailsService
    ├─ InMemoryUserAuthoritiesResolver
    └─ Simple default authentication
```

### OidcUserManagementAutoConfiguration
```
Activated: When OAuth2/OIDC properties configured

Provides:
    ├─ OidcUserService (JWT authorities variant)
    ├─ JwtAuthoritiesExtractor
    ├─ OidcBearerTokenAuthenticationFilter
    ├─ OidcAuthenticationSuccessHandler
    ├─ OidcLogoutHandler
    └─ OidcUserAuditService
```

---

## Authentication Decision Tree

```
                    Incoming Request
                           │
                           ▼
                    ┌─────────────┐
                    │DoS Filter OK?│
                    └──────┬──────┘
                           │ Yes
                           ▼
                    ┌─────────────┐
                    │Anonymous?   │
                    └──────┬──────┘
                  Yes/No   │
                    │      │
        ┌───────────┴──────▼──────────────┐
        │                                  │
        ▼                                  ▼
   Allow Anonymous            Pre-Auth Extract
   (ControllerPreAuthenticated
   AnonymousFilter)          ┌────────────────────────┐
        │                    │Has Security Token?     │
        │                    └──────┬────────┬────────┘
        │              Yes          │        │ No
        │                    ┌──────▼┐  ┌───▼───┐
        │                    │Token  │  │Header?│
        │                    │Filter │  └──┬────┘
        │                    └──────────┬──┘
        │                               │
        │                    ┌──────────▼──┐
        │                    │Gateway Token?│
        │                    └──────┬───────┘
        │                           │
        │                    ┌──────▼──────────┐
        │                    │JWT Bearer Token?│
        │                    └──────┬───────────┘
        │                           │
        │                           ▼
        │              OidcBearerTokenAuthenticationFilter
        │                    │
        │                    ├─ Validate JWT
        │                    ├─ Extract Authorities
        │                    └─ Load OIDC User
        │                           │
        └───────────────┬───────────┘
                        │
                        ▼
            SecurityContextTenantAware
            (Store in ThreadLocal)
                        │
                        ▼
            Request Processed
            (with authentication)
```

---

## Security Properties Configuration Map

### Application Configuration Properties

| Property | Environment | Value | Impact |
|----------|-------------|-------|--------|
| `hawkbit.server.ddi.security.authentication.anonymous.enabled` | Local | true | Anonymous downloads allowed |
| | Prod | false | Requires authentication |
| `hawkbit.server.ddi.security.authentication.targettoken.enabled` | All | true | Device tokens accepted |
| `hawkbit.server.ddi.security.authentication.gatewaytoken.enabled` | Local | true | Gateway tokens accepted |
| | Prod | false | Gateway tokens rejected |
| `spring.security.user.name` | All | admin | Default admin user |
| `spring.security.user.password` | All | {noop}admin | No-op password encoder |
| `spring.security.oauth2.client.registration.oidc.*` | Local | Custom | OIDC client configuration |
| `hawkbit.server.security.dos.maxArtifactSize` | Local | 100MB | Artifact size limit |
| | Prod | 1GB | Larger limit |
| `hawkbit.server.security.dos.maxArtifactStorage` | Local | 100GB | Storage limit |
| | Prod | 100GB | Larger limit |

---

## HTTP Filter Chain Configuration

### Filter Order (Spring Security Standard)

```
Order 0:   DosFilter
           └─ Rejects requests exceeding size/storage limits

Order 100: HttpControllerPreAuthenticateSecurityTokenFilter
           └─ Extracts token from request

Order 150: HttpControllerPreAuthenticatedSecurityHeaderFilter
           └─ Extracts auth from headers

Order 200: OidcBearerTokenAuthenticationFilter
           └─ Validates JWT bearer tokens

Order 250: ... (Spring Security standard filters)

Order 1000: ExceptionTranslationFilter
            └─ OidcRestAuthenticationEntryPoint for failed auth
```

---

## Role-Based Access Control (RBAC)

### Permission Management
```
PermissionService
    ├─ Checks user permissions
    ├─ Tenant-scoped permission evaluation
    └─ Uses SecurityContextTenantAware for context

User → [UserPrincipal + Authorities] × Tenant
    ├─ Each tenant has different roles
    └─ Authority evaluation per-tenant
```

### Authority Sources

1. **In-Memory**: `InMemoryUserAuthoritiesResolver`
   - Properties-based role definitions
   - Simple default implementation

2. **JWT/OIDC**: `JwtAuthoritiesExtractor`
   - Roles from JWT claims
   - Dynamic authority resolution
   - Tenant-aware claim parsing

---

## Audit & Logging

```
SpringSecurityAuditorAware
    ├─ Captures authenticated username
    ├─ Available to @CreatedBy, @LastModifiedBy
    └─ Automatic entity audit

OidcUserAuditService
    ├─ Logs OAuth2 user events
    ├─ Tracks login/logout
    └─ Compliance audit trail
```

---

## Error Handling

```
OidcRestAuthenticationEntryPoint
    ├─ Handles unauthenticated REST requests
    ├─ Returns 401 Unauthorized
    └─ error_description in JSON response

OAuth2AccessDeniedException
    ├─ OAuth2 authorization failures
    ├─ Wrapped in ExceptionInfo
    └─ Includes error code & description

OidcLogoutSuccessHandler
    ├─ Post-logout redirect
    └─ Cleans up session
```

---

## Key Security Features Summary

| Feature | Implementation | Scope |
|---------|---------------|----|
| **Multi-Tenancy** | Tenant-aware tokens, context, authorities | All layers |
| **DoS Protection** | Size/Storage limits, DosFilter | HTTP entry |
| **Token Validation** | JWT validation, signature check | HTTP layer |
| **Authority Resolution** | Per-tenant role mapping | Request processing |
| **Audit Trail** | SpringSecurityAuditorAware + OidcUserAuditService | Request/response |
| **OIDC Integration** | Full OAuth2 flow, JWT bearer tokens | Authentication |
| **Anonymous Access** | Controlled, configurable per environment | Selective endpoints |
| **Gateway Token Trust** | Source-based trust, no re-validation | Gateway requests |
| **Header-based Auth** | Custom security headers | Alternative auth |

---

## Environment Differences

```
LOCAL ENVIRONMENT:
├─ All authentication methods enabled
├─ Anonymous access allowed
├─ Gateway tokens accepted
├─ OIDC connected to preprod
└─ No DoS limits enforced

PRODUCTION ENVIRONMENT:
├─ Selected authentication methods
├─ Anonymous access disabled
├─ Gateway tokens rejected
├─ OIDC connected to production
├─ Strict DoS limits (1GB/100GB)
└─ Password encoding enforced
```

---

## Related Configuration

### Autoconfiguration Conditional Logic

```
SecurityAutoConfiguration (always loaded)
    ├─ Loads core Security Beans
    │
├─ IF (MultiUserProperties.enabled=true)
│  │
│  ▼ InMemoryUserManagementAutoConfiguration
│  │   (loads in-memory auth)
│  │
├─ IF (OAuth2 Properties detected)
│  │
│  ▼ OidcUserManagementAutoConfiguration
│     (loads OIDC auth)
│
├─ IF (@EnableHawkbitManagedSecurity)
│  │
│  ▼ SecurityManagedConfiguration
│     (@EnableWebSecurity)
│
└─ Result: Composed security configuration
```

---

## Technology Stack

- **Framework**: Spring Security 5.x+
- **Authentication**: OAuth2/OIDC, JWT Tokens
- **OIDC Provider**: Ping Federate (idfed-preprod.mpsa.com)
- **JWT**: Standard RFC 7519 format
- **Multi-Tenancy**: Tenant context in SecurityContext
- **Audit**: Spring Data AuditorAware
- **Protection**: DoS filter with request size limiting
