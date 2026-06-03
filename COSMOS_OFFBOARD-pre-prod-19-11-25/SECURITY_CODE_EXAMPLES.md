# Security Configuration Examples & Code Snippets

## 1. Core Security Configuration Pattern

### @EnableWebSecurity Annotation Pattern
```java
@Configuration
@EnableWebSecurity
public class SecurityManagedConfiguration {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilter(/* authentication filter */)
            .addFilter(/* authorization filter */);
        
        return http.build();
    }
}
```

Location: `hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/SecurityManagedConfiguration.java`

---

## 2. Multi-Configuration Approach for Different APIs

```java
@Configuration
public class SecurityManagedConfiguration {
    
    // REST API Configuration
    @Configuration
    static class HttpSecurityConfiguration {
        @Bean
        public SecurityFilterChain restSecurityFilterChain(HttpSecurity http) {
            // /api/** endpoints
        }
    }
    
    // Management API Configuration
    @Configuration
    @EnableWebSecurity
    static class HttpMgmtSecurityConfiguration {
        @Bean
        public SecurityFilterChain mgmtSecurityFilterChain(HttpSecurity http) {
            // /mgmt/** endpoints
        }
    }
    
    // Device/Controller API Configuration
    @Configuration
    @EnableWebSecurity
    static class HttpControllerSecurityConfiguration {
        @Bean
        public SecurityFilterChain controllerSecurityFilterChain(HttpSecurity http) {
            // /controller/** endpoints
        }
    }
}
```

---

## 3. Authentication Filter Registration

### Pre-Authentication Filter Example
```java
@Bean
public HttpControllerPreAuthenticateSecurityTokenFilter 
        controllerPreAuthenticateSecurityTokenFilter() {
    return new HttpControllerPreAuthenticateSecurityTokenFilter(
        /* dependencies */
    );
}

// In SecurityFilterChain configuration
http.addFilter(controllerPreAuthenticateSecurityTokenFilter());
```

---

## 4. OIDC/OAuth2 Configuration

### Application Properties Configuration
```properties
# OIDC Client Registration
spring.security.oauth2.client.registration.oidc.client-id=MUWABNBDNBCKBAXEQPYRQNFCECFPKVWD
spring.security.oauth2.client.registration.oidc.client-secret=<secret>
spring.security.oauth2.client.registration.oidc.scope=openid,prd:aug,profile
spring.security.oauth2.client.registration.oidc.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

# OIDC Provider Configuration
spring.security.oauth2.client.provider.oidc.issuer-uri=https://idfed-preprod.mpsa.com:443
spring.security.oauth2.client.provider.oidc.authorization-uri=https://idfed-preprod.mpsa.com:443/as/authorization.oauth2
spring.security.oauth2.client.provider.oidc.token-uri=https://idfed-preprod.mpsa.com:443/as/token.oauth2
spring.security.oauth2.client.provider.oidc.user-info-uri=https://idfed-preprod.mpsa.com:443/idp/userinfo.openid
spring.security.oauth2.client.provider.oidc.jwk-set-uri=https://idfed-preprod.mpsa.com:443/pf/JWKS
spring.security.oauth2.client.provider.oidc.logout-uri=https://idfed-preprod.mpsa.com/idp/startSLO.ping
```

### Bean Configuration for OIDC
```java
@Configuration
public class OidcUserManagementAutoConfiguration {
    
    @Bean
    public JwtAuthoritiesValidator jwtAuthoritiesValidator() {
        return new JwtAuthoritiesValidator();
    }
    
    @Bean
    public JwtAuthoritiesExtractor jwtAuthoritiesExtractor() {
        return new JwtAuthoritiesExtractor();
    }
    
    @Bean
    public JwtAuthoritiesOidcUserService jwtAuthoritiesOidcUserService(
            JwtAuthoritiesExtractor extractor) {
        return new JwtAuthoritiesOidcUserService(extractor);
    }
    
    @Bean
    public OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter(
            JwtAuthoritiesValidator validator) {
        return new OidcBearerTokenAuthenticationFilter(validator);
    }
}
```

---

## 5. JWT Token Handling

### JWT Parsing Example
```java
// In JwtUtil.java
public class JwtUtil {
    
    public static Claims parseJwt(String token, String secretKey) {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    // Called by OidcBearerTokenAuthenticationFilter
    public static Collection<String> extractAuthorities(Claims claims) {
        return (Collection<String>) claims.get("authorities");
    }
}
```

### JWT Validation
```java
// In JwtAuthoritiesValidator.java
public class JwtAuthoritiesValidator {
    
    public boolean isTokenValid(String token, String issuer) {
        try {
            Claims claims = parseJwt(token);
            
            // Validate token not expired
            if (claims.getExpiration().before(new Date())) {
                return false;  // Expired
            }
            
            // Validate issuer
            if (!claims.getIssuer().equals(issuer)) {
                return false;  // Invalid issuer
            }
            
            // Validate signature (handled by JWT parser)
            return true;
            
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature", e);
            return false;
        }
    }
}
```

---

## 6. Tenant-Aware Authentication

### Tenant Context in Token
```java
// In TenantUserPasswordAuthenticationToken.java
public class TenantUserPasswordAuthenticationToken 
        extends UsernamePasswordAuthenticationToken {
    
    private final String tenant;
    
    public TenantUserPasswordAuthenticationToken(
            Object principal, 
            Object credentials, 
            String tenant) {
        super(principal, credentials);
        this.tenant = tenant;
    }
    
    public String getTenant() {
        return tenant;
    }
}
```

### Tenant Extraction from JWT
```java
// In JwtAuthoritiesExtractor.java
public UserTenantAuthorities extractTenantAuthorities(Claims claims) {
    String tenant = (String) claims.get("tenant");
    List<String> authorities = 
        (List<String>) claims.get("authorities");
    
    return new UserTenantAuthorities()
        .setTenant(tenant)
        .setRoles(authorities);
}
```

### Tenant-Aware Security Context
```java
// In SecurityContextTenantAware.java
@Component
public class SecurityContextTenantAware implements TenantAware {
    
    @Override
    public String getCurrentTenant() {
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (auth instanceof TenantUserPasswordAuthenticationToken) {
            return ((TenantUserPasswordAuthenticationToken) auth)
                .getTenant();
        }
        return null;
    }
}
```

---

## 7. Authentication Filter Implementation

### Custom Filter Example
```java
// Base structure for HttpControllerPreAuthenticateSecurityTokenFilter
public class HttpControllerPreAuthenticateSecurityTokenFilter 
        extends AbstractHttpControllerAuthenticationFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain chain) 
            throws ServletException, IOException {
        
        // 1. Extract token from request
        String token = extractToken(request);
        
        if (token != null) {
            try {
                // 2. Validate token
                Claims claims = JwtUtil.parseJwt(token);
                
                // 3. Extract tenant and authorities
                String tenant = (String) claims.get("tenant");
                Collection<String> authorities = 
                    JwtAuthoritiesExtractor.extract(claims);
                
                // 4. Create authentication token
                TenantUserPasswordAuthenticationToken authToken =
                    new TenantUserPasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        tenant
                    );
                authToken.setAuthenticated(true);
                authToken.setDetails(
                    TenantAwareAuthenticationDetails.from(claims)
                );
                
                // 5. Set in security context
                SecurityContextHolder.getContext()
                    .setAuthentication(authToken);
                
            } catch (Exception e) {
                logger.error("Token validation failed", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("X-Security-Token");
        return header != null ? header : null;
    }
}
```

---

## 8. OIDC Authentication Flow

### OAuth2 Success Handler
```java
// In OidcAuthenticationSuccessHandler.java
@Component
public class OidcAuthenticationSuccessHandler 
        extends SavedRequestAwareAuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) 
            throws ServletException, IOException {
        
        // 1. Extract OAuth2 user details
        OAuth2User oAuth2User = 
            (OAuth2User) authentication.getPrincipal();
        
        // 2. Load user authorities
        Collection<String> authorities = 
            JwtAuthoritiesExtractor.extract(oAuth2User.getAttributes());
        
        // 3. Audit log
        OidcUserAuditService.audit(
            oAuth2User.getName(), 
            "LOGIN_SUCCESS"
        );
        
        // 4. Redirect to saved request or default
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
```

### Bearer Token Authentication Filter
```java
// In OidcBearerTokenAuthenticationFilter.java
public class OidcBearerTokenAuthenticationFilter 
        implements UserAuthenticationFilter, Filter {
    
    @Override
    public void doFilter(
            ServletRequest request, 
            ServletResponse response, 
            FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // Validate JWT
                if (jwtValidator.isTokenValid(token)) {
                    // Extract claims and create authentication
                    Claims claims = JwtUtil.parseJwt(token);
                    
                    Collection<? extends GrantedAuthority> authorities =
                        JwtAuthoritiesExtractor.extractAuthorities(claims);
                    
                    OAuth2AuthenticatedPrincipal principal =
                        new DefaultOAuth2AuthenticatedPrincipal(
                            claims.getSubject(),
                            claims,
                            authorities
                        );
                    
                    // Set authentication in context
                    BearerTokenAuthentication authentication =
                        new BearerTokenAuthentication(
                            principal, 
                            token, 
                            authorities
                        );
                    
                    SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                }
            } catch (Exception e) {
                logger.error("Bearer token validation failed", e);
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

---

## 9. Authority Resolution

### In-Memory Authority Resolver
```java
// In InMemoryUserAuthoritiesResolver.java
@Component
public class InMemoryUserAuthoritiesResolver 
        implements UserAuthoritiesResolver {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    public Collection<GrantedAuthority> resolveAuthorities(
            String username) {
        
        UserDetails userDetails = userDetailsService
            .loadUserByUsername(username);
        
        return userDetails.getAuthorities();
    }
}
```

### JWT Authority Extractor
```java
// In JwtAuthoritiesExtractor.java
public class JwtAuthoritiesExtractor {
    
    public static Collection<String> extract(Claims claims) {
        List<String> authorities = 
            (List<String>) claims.get("authorities");
        
        return authorities != null ? 
            authorities : Collections.emptyList();
    }
    
    public static UserTenantAuthorities extractWithTenant(
            Claims claims) {
        
        String tenant = (String) claims.get("tenant");
        List<String> authorities = extract(claims);
        
        return new UserTenantAuthorities()
            .setTenant(tenant)
            .setRoles(authorities);
    }
}
```

---

## 10. Permission Service Usage

### Authorization Checks in Business Logic
```java
// In PermissionService.java
@Component
public class PermissionService {
    
    @Autowired
    private SecurityContextTenantAware tenantAware;
    
    public boolean hasPermission(String action) {
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (auth == null) return false;
        
        // Check if user has authority for action
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(action));
    }
    
    public boolean hasPermissionForTenant(
            String action, 
            String tenant) {
        
        String currentTenant = tenantAware.getCurrentTenant();
        
        // Only allow access to current tenant
        if (!tenant.equals(currentTenant)) {
            return false;
        }
        
        return hasPermission(action);
    }
}
```

---

## 11. Audit Logging

### Auditor Aware Implementation
```java
// In SpringSecurityAuditorAware.java
@Component
public class SpringSecurityAuditorAware 
        implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = 
            SecurityContextHolder.getContext()
                .getAuthentication();
        
        if (authentication == null || 
            !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        return Optional.of(
            authentication.getName()
        );
    }
}
```

### Usage in Entity
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class SoftwareModule {
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime modifiedDate;
}
```

### OIDC User Audit Service
```java
// In OidcUserAuditService.java
@Component
public class OidcUserAuditService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    public void auditUserLogin(String username) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction("LOGIN");
        log.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }
    
    public void auditUserLogout(String username) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction("LOGOUT");
        log.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }
}
```

---

## 12. DoS Protection Filter

### Size Limiting Implementation
```java
// In DosFilter.java
public class DosFilter extends OncePerRequestFilter {
    
    @Autowired
    private ApplicationProperties appProperties;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) 
            throws ServletException, IOException {
        
        long contentLength = request.getContentLength();
        long maxSize = appProperties.getSecurity()
            .getDos()
            .getMaxArtifactSize();
        
        if (contentLength > maxSize) {
            response.sendError(
                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                "Artifact size exceeds maximum allowed: " + maxSize
            );
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 13. Configuration Properties Binding

### Security Properties Classes
```java
// In HawkbitSecurityProperties.java
@ConfigurationProperties("hawkbit.server.security")
@Component
public class HawkbitSecurityProperties {
    
    private Dos dos = new Dos();
    
    public static class Dos {
        private long maxArtifactSize;
        private long maxArtifactStorage;
        
        // getters/setters
    }
}

// In DdiSecurityProperties.java
@ConfigurationProperties("hawkbit.server.ddi.security.authentication")
@Component
public class DdiSecurityProperties {
    
    private Anonymous anonymous = new Anonymous();
    private TargetToken targetToken = new TargetToken();
    private GatewayToken gatewayToken = new GatewayToken();
    
    public static class Anonymous {
        private boolean enabled;
        // getters/setters
    }
    // Similar inner classes for targetToken, gatewayToken
}
```

### Usage in Conditional Configuration
```java
@Configuration
@ConditionalOnProperty(
    name = "hawkbit.server.im.enabled",
    havingValue = "true"
)
public class InMemoryUserManagementAutoConfiguration {
    // Configuration that loads only if property is true
}
```

---

## Summary Table: Key Components & Their Responsibilities

| Component | Location | Responsibility |
|-----------|----------|-----------------|
| `SecurityManagedConfiguration` | autoconfigure/security | Main config with @EnableWebSecurity |
| `DosFilter` | http-security/security | DoS protection, request size validation |
| `HttpControllerPreAuthenticateSecurityTokenFilter` | http-security/security | Extract token from header |
| `HttpControllerPreAuthenticatedSecurityHeaderFilter` | http-security/security | Extract auth from custom headers |
| `OidcBearerTokenAuthenticationFilter` | security-integration/security | Validate JWT bearer tokens |
| `JwtAuthoritiesExtractor` | security-integration | Extract user authorities from JWT |
| `JwtAuthoritiesValidator` | security-integration | Validate JWT token signature/expiry |
| `PreAuthTokenSourceTrustAuthenticationProvider` | security-integration | Validate pre-authenticated tokens |
| `SecurityContextTenantAware` | security-core/security | Store tenant context in security |
| `PermissionService` | security-core/im/authentication | Authorization checks |
| `SpringSecurityAuditorAware` | security-core/security | Capture action auditor |
| `OidcUserAuditService` | security-integration | Audit OAuth2 events |

---

## Configuration Loading Order

```
1. Start.java (@SpringBootApplication)
   │
   ├─ LogProperties (loading banner)
   │
   ├─ HawkbitSecurityProperties (loads hawkbit.server.security.*)
   ├─ DdiSecurityProperties (loads hawkbit.server.ddi.security.*)
   ├─ OidcSecurityProperties (loads hawkbit.server.security.oidc.*)
   │
   ├─ SecurityAutoConfiguration
   │  │
   │  ├─ InMemoryUserManagementAutoConfiguration (@if enabled)
   │  │  └─ InMemoryUserAuthoritiesResolver, UserDetailsService
   │  │
   │  ├─ OidcUserManagementAutoConfiguration (@if OAuth2 properties)
   │  │  └─ JwtAuthoritiesExtractor, OidcBearerTokenAuthenticationFilter, etc.
   │  │
   │  └─ SecurityManagedConfiguration (@EnableWebSecurity)
   │     └─ Multiple nested @Configuration per API endpoint
   │
   └─ Application Ready
```
