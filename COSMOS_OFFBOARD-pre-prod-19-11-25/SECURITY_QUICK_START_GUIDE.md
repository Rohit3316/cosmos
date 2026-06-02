# Security & Authentication - Developer Quick Start Guide

## For Developers Working with COSMOS Management API Security

---

## 1. Quick Facts

| Item | Details |
|------|---------|
| **Main Entry Point** | `cosmos-mgmt-api/src/main/java/com/stellantis/cosmos/mgmt/app/Start.java` |
| **Security Framework** | Spring Security 5.x + Spring OAuth2 |
| **Authentication Methods** | Token-based, OAuth2/OIDC, Header-based, Gateway |
| **Multi-Tenancy** | Full tenant-aware context throughout |
| **Audit Logging** | Automatic via SpringSecurityAuditorAware |
| **OIDC Provider** | Ping Federate (idfed-preprod.mpsa.com) |
| **Default User** | admin / {noop}admin (local only) |

---

## 2. Where to Find Things

### If you need to...

**Add a new authentication method:**
1. Create filter in `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/`
2. Extend `AbstractControllerAuthenticationFilter` or `AbstractHttpControllerAuthenticationFilter`
3. Register in `SecurityManagedConfiguration`

**Configure security properties:**
1. Add property to `application-[env].properties`
2. Create corresponding `@ConfigurationProperties` class in `hawkbit-security-core`
3. Use in `SecurityAutoConfiguration` or `SecurityManagedConfiguration`

**Add authorization/permission checks:**
1. Inject `PermissionService` from `hawkbit-security-core/im/authentication/PermissionService.java`
2. Call `hasPermission()` or `hasPermissionForTenant()`

**Track auditable actions:**
1. Mark entity with `@Entity` + `@EntityListeners(AuditingEntityListener.class)`
2. Use `@CreatedBy`, `@LastModifiedBy` annotations
3. Automatically populated via `SpringSecurityAuditorAware`

**Handle OIDC/JWT tokens:**
1. See `JwtUtil` (location: `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/util/JwtUtil.java`)
2. Token extraction: `JwtAuthoritiesExtractor`
3. Token validation: `JwtAuthoritiesValidator`

**Get current tenant:**
```java
@Autowired
private SecurityContextTenantAware tenantAware;

public void someMethod() {
    String currentTenant = tenantAware.getCurrentTenant();
    // Use current tenant for data filtering
}
```

**Get current authenticated user:**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
Collection<GrantedAuthority> authorities = auth.getAuthorities();
```

---

## 3. Configuration Quick Reference

### Local Development
```properties
# User & Security
spring.security.user.name=admin
spring.security.user.password={noop}admin

# DDI Authentication (all enabled for dev)
hawkbit.server.ddi.security.authentication.anonymous.enabled=true
hawkbit.server.ddi.security.authentication.targettoken.enabled=true
hawkbit.server.ddi.security.authentication.gatewaytoken.enabled=true

# OIDC/OAuth2 Connection
spring.security.oauth2.client.registration.oidc.client-id=MUWABNBDNBCKBAXEQPYRQNFCECFPKVWD
spring.security.oauth2.client.registration.oidc.client-secret=<configured>
spring.security.oauth2.client.provider.oidc.issuer-uri=https://idfed-preprod.mpsa.com:443

# DoS Protection (relaxed for dev)
hawkbit.server.security.dos.maxArtifactSize=100000000
hawkbit.server.security.dos.maxArtifactStorage=100000000000
```

### Production
```properties
# User & Security
spring.security.user.name=admin
# Password from environment variable

# DDI Authentication (restrictive)
hawkbit.server.ddi.security.authentication.anonymous.enabled=false
hawkbit.server.ddi.security.authentication.targettoken.enabled=true
hawkbit.server.ddi.security.authentication.gatewaytoken.enabled=false

# OIDC/OAuth2 Connection (production OIDC)
spring.security.oauth2.client.provider.oidc.issuer-uri=https://idfed-prod.mpsa.com:443

# DoS Protection (strict limits)
hawkbit.server.security.dos.maxArtifactSize=1073741824
hawkbit.server.security.dos.maxArtifactStorage=107374182400
```

---

## 4. Common Tasks

### Task: Add New Tenant to Multi-Tenant Setup

1. **In JWT Token**: Include `"tenant": "new-tenant-id"` in JWT claims
2. **In Database**: Create tenant-scoped data
3. **Automatic**: `SecurityContextTenantAware` will handle tenant isolation
4. **Verification**: All queries automatically filtered by current tenant

### Task: Enable/Disable Authentication Method

Edit `application-[env].properties`:
```properties
# Enable/disable token authentication
hawkbit.server.ddi.security.authentication.targettoken.enabled=true|false

# Enable/disable gateway token
hawkbit.server.ddi.security.authentication.gatewaytoken.enabled=true|false

# Enable/disable anonymous
hawkbit.server.ddi.security.authentication.anonymous.enabled=true|false
```

### Task: Create Protected Endpoint

```java
@RestController
@RequestMapping("/api/protected")
public class ProtectedController {
    
    @Autowired
    private PermissionService permissionService;
    
    @GetMapping("/resource")
    public ResponseEntity<?> getResource() {
        
        // Check permission
        if (!permissionService.hasPermission("READ_RESOURCE")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Check tenant if needed
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Proceed with logic
        return ResponseEntity.ok("Resource data");
    }
}
```

### Task: Debug Authentication Issues

1. **Check Filter Chain**: Look for DoS rejection (413 status)
2. **Check Token**: Validate JWT at `jwt.io`
3. **Check Tenant**: Ensure tenant header/claim present
4. **Check Authority**: Verify user has required permission
5. **Check Context**: Add logging to `SecurityContextTenantAware`

```java
// Add this to SecurityContextTenantAware for debugging
public String getCurrentTenant() {
    String tenant = extractTenant();
    log.debug("Current tenant resolved: {}", tenant);
    return tenant;
}
```

---

## 5. Key Classes to Know

### Must Know Classes
| Class | Purpose | When to Use |
|-------|---------|------------|
| `SecurityContextHolder` | Access current security context | Anywhere you need current user/tenant |
| `Authentication` | Represents authenticated principal | Checking permissions, getting user info |
| `GrantedAuthority` | User authority/role | Permission checks |
| `SecurityContextTenantAware` | Get current tenant | Multi-tenant data filtering |
| `PermissionService` | Authorization checks | Before executing operations |

### Filter Chain Classes (know them in order)
1. `DosFilter` - Stop oversized requests
2. `HttpControllerPreAuthenticateSecurityTokenFilter` - Extract token
3. `HttpControllerPreAuthenticatedSecurityHeaderFilter` - Extract from header
4. `OidcBearerTokenAuthenticationFilter` - Validate JWT
5. Spring Security default filters
6. `OidcRestAuthenticationEntryPoint` - Handle auth failures

---

## 6. Testing Security

### Unit Test Example
```java
@SpringBootTest
class SecurityTest {
    
    @Autowired
    private SecurityContextTenantAware tenantAware;
    
    @Test
    @WithMockUser(username = "testuser", authorities = {"ADMIN"})
    public void testAuthenticatedRequest() {
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("testuser");
        assertThat(auth.getAuthorities())
            .extracting("authority")
            .contains("ADMIN");
    }
    
    @Test
    public void testTenantContext() {
        // Setup mock with tenant
        // Set in SecurityContext
        
        String tenant = tenantAware.getCurrentTenant();
        
        assertThat(tenant).isNotEmpty();
    }
}
```

### Integration Test Example
```java
@SpringBootTest
class OidcIntegrationTest {
    
    @Test
    public void testOidcFlow() throws Exception {
        // Setup mock OIDC provider
        // Send OAuth2 request
        // Verify JWT token received
        // Check authorities extracted
    }
}
```

---

## 7. Troubleshooting Guide

| Problem | Diagnosis | Solution |
|---------|-----------|----------|
| 401 Unauthorized | Check if token valid | Verify JWT not expired, signature correct |
| 403 Forbidden | User lacks permission | Check user authorities in JWT claims |
| DoS rejection (413) | Request too large | Check `maxArtifactSize` property |
| Tenant mismatch | Wrong tenant in context | Verify tenant in token matches request |
| Authentication not working | Filter not registered | Check `SecurityManagedConfiguration` bean |
| OIDC not connecting | Provider endpoint issue | Check `issuer-uri` and network connectivity |

---

## 8. Common Properties to Modify

### Security Properties
```properties
# Disable authentication for testing
spring.security.filter.order=1
hawkbit.server.im.enabled=false

# Change DoS limits
hawkbit.server.security.dos.maxArtifactSize=<bytes>
hawkbit.server.security.dos.maxArtifactStorage=<bytes>

# Control authentication methods
hawkbit.server.ddi.security.authentication.*.enabled=true|false

# OIDC configuration
spring.security.oauth2.client.provider.oidc.issuer-uri=<url>
spring.security.oauth2.client.registration.oidc.client-id=<id>
```

---

## 9. File Paths Cheat Sheet

```
Core Security:
  hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/
    → HawkbitSecurityProperties.java
    → SecurityTokenGenerator.java
    → SystemSecurityContext.java
  
Integration Filters:
  hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/
    → AbstractControllerAuthenticationFilter.java
    → PreAuthTokenSourceTrustAuthenticationProvider.java
  
OIDC:
  hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/
    → authentication/JwtAuthoritiesExtractor.java
    → authentication/OidcBearerTokenAuthenticationFilter.java
  
HTTP Filters:
  hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/
    → AbstractHttpControllerAuthenticationFilter.java
    → DosFilter.java
  
Auto-Configuration:
  hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/
    → SecurityAutoConfiguration.java
    → SecurityManagedConfiguration.java
```

---

## 10. Adding a New Security Feature

### Step-by-Step: Add Header-Based Token Validation

1. **Create Filter** (in `hawkbit-http-security`)
```java
public class CustomHeaderAuthenticationFilter 
        extends AbstractHttpControllerAuthenticationFilter {
    
    @Override
    protected void doFilterInternal(...) {
        String customHeader = request.getHeader("X-Custom-Auth");
        // Validate and set authentication
    }
}
```

2. **Register Bean** (in `SecurityAutoConfiguration`)
```java
@Bean
public CustomHeaderAuthenticationFilter customHeaderFilter() {
    return new CustomHeaderAuthenticationFilter();
}
```

3. **Add to Filter Chain** (in `SecurityManagedConfiguration`)
```java
.addFilter(customHeaderFilter())
```

4. **Add Properties** (in `application-env.properties`)
```properties
hawkbit.server.security.custom.header.enabled=true
```

5. **Add Configuration Class**
```java
@ConfigurationProperties("hawkbit.server.security.custom.header")
public class CustomHeaderProperties {
    private boolean enabled;
    // getters/setters
}
```

6. **Test It**
```bash
curl -H "X-Custom-Auth: my-token" http://localhost:8080/api/endpoint
```

---

## 11. Key Takeaways

✅ **Do:**
- Use `SecurityContextHolder` for current user/tenant
- Use `PermissionService` for authorization
- Configure via properties, not hardcoding
- Test with mock security context
- Use `@EntityListeners` for audit trails
- Follow tenant-aware patterns for multi-tenancy

❌ **Don't:**
- Bypass security context in business logic
- Store sensitive data in tokens (use claims only)
- Hardcode usernames/passwords
- Ignore tenant context in queries
- Mix authentication concerns in business code
- Assume request is authenticated (always check)

---

## 12. Resources

**Files Created (in workspace root):**
- `SECURITY_AND_AUTH_FINDINGS.md` - Comprehensive findings
- `SECURITY_FILE_INDEX.md` - File organization by category
- `SECURITY_ARCHITECTURE.md` - Architecture diagrams and flows
- `SECURITY_CODE_EXAMPLES.md` - Code snippets and examples

**Additional Resources:**
- Spring Security Documentation: https://spring.io/projects/spring-security
- OAuth2/OIDC Specs: https://datatracker.ietf.org/doc/html/rfc6749
- JWT Reference: https://tools.ietf.org/html/rfc7519

---

## 13. Quick Verification Checklist

Before deploying security changes:

- [ ] All authentication filters registered in `SecurityManagedConfiguration`
- [ ] Properties added to all `application-*.properties` files
- [ ] Multi-tenant context preserved throughout filter chain
- [ ] Audit logging tested (check entity listeners)
- [ ] OIDC provider connectivity verified
- [ ] JWT token validation tested with actual provider
- [ ] DoS protection limits appropriate for environment
- [ ] Authorization checks in all protected endpoints
- [ ] Error handling returns appropriate HTTP status codes
- [ ] Tenant isolation verified with multi-tenant data
