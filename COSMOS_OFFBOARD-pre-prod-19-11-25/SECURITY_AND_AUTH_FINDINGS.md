# Security and Authentication Code Analysis

## Overview
This document provides a comprehensive overview of all authentication, security, and configuration-related code in the COSMOS application, specifically for the management API and security modules.

---

## 1. COSMOS Management API Entry Point

### Application Startup
- **File**: [cosmos-mgmt-api/src/main/java/com/stellantis/cosmos/mgmt/app/Start.java](cosmos-mgmt-api/src/main/java/com/stellantis/cosmos/mgmt/app/Start.java)
  - Main Spring Boot application class with `@SpringBootApplication` annotation
  - Entry point for the Management API service

---

## 2. Security Configuration Files (Autoconfiguration)

### Location: `hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/`

#### Core Security Configuration Classes

1. **[SecurityAutoConfiguration.java](hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/SecurityAutoConfiguration.java)**
   - Main security auto-configuration class
   - Annotation: `@Configuration`
   - Bean definitions for security components including:
     - Security token generators
     - Authentication providers
     - User authority resolvers
     - Auditor services

2. **[SecurityManagedConfiguration.java](hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/SecurityManagedConfiguration.java)**
   - Annotation: `@Configuration`, `@EnableWebSecurity`
   - Nested security configurations for:
     - REST API security
     - Management API security
     - Controller/DDI API security
   - Multiple inner configuration classes for different API endpoints

3. **[EnableHawkbitManagedSecurityConfiguration.java](hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/EnableHawkbitManagedSecurityConfiguration.java)**
   - Enables Hawkbit-managed security configuration
   - Provides conditional auto-configuration

#### User Management Configurations

4. **[InMemoryUserManagementAutoConfiguration.java](hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/InMemoryUserManagementAutoConfiguration.java)**
   - Annotation: `@Configuration`
   - In-memory user authentication setup
   - Configures in-memory user details services

5. **[OidcUserManagementAutoConfiguration.java](hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/OidcUserManagementAutoConfiguration.java)**
   - Annotation: `@Configuration`
   - OpenID Connect (OIDC) user management configuration
   - Manages OAuth2/OIDC authentication flows
   - Bean definitions for:
     - JWT authorities extraction
     - OIDC user services
     - Authentication success handlers
     - Logout handlers

6. **[MultiUserProperties.java](hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/MultiUserProperties.java)**
   - Annotation: `@ConfigurationProperties("hawkbit.server.im")`
   - Properties for multi-tenant user management

---

## 3. Core Security Classes

### Location: `hawkbit-security-core/src/main/java/org/eclipse/hawkbit/`

#### Security Properties & Configuration

1. **[HawkbitSecurityProperties.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/HawkbitSecurityProperties.java)**
   - General Hawkbit security properties configuration
   - Manages core security-related configuration values

2. **[DdiSecurityProperties.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/DdiSecurityProperties.java)**
   - DDI (Device Management) security properties
   - Handles DDI-specific authentication settings

3. **[OidcSecurityProperties.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/OidcSecurityProperties.java)**
   - OpenID Connect security-related properties
   - Manages OIDC configuration parameters

#### Authentication & Token Management

4. **[SecurityTokenGenerator.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/SecurityTokenGenerator.java)**
   - Generates security tokens for authentication
   - Creates authentication credentials

5. **[SecurityContextTenantAware.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/SecurityContextTenantAware.java)**
   - Implements `TenantAware` interface
   - Provides tenant-aware security context management
   - Handles multi-tenant security isolation

6. **[SystemSecurityContext.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/SystemSecurityContext.java)**
   - Manages system-level security context
   - Handles system authentication

#### User Management

7. **[InMemoryUserAuthoritiesResolver.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/InMemoryUserAuthoritiesResolver.java)**
   - Implements `UserAuthoritiesResolver` interface
   - Resolves user authorities from in-memory storage
   - Maps user roles and permissions

#### Audit & Observability

8. **[SpringSecurityAuditorAware.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/SpringSecurityAuditorAware.java)**
   - Implements Spring's `AuditorAware<String>` interface
   - Provides audit information from Security context
   - Tracks who performed actions for compliance

#### Security Constants

9. **[SecurityConstants.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/security/SecurityConstants.java)**
   - Contains security-related constants
   - Defines security policy values

#### Identity Management

### Location: `hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/`

10. **[UserPrincipal.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/UserPrincipal.java)**
    - Extends Spring's `User` class
    - Represents authenticated user principal
    - Contains user identity information

11. **[TenantUserPasswordAuthenticationToken.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/TenantUserPasswordAuthenticationToken.java)**
    - Extends `UsernamePasswordAuthenticationToken`
    - Multi-tenant username/password authentication token
    - Handles tenant-specific authentication

12. **[TenantAwareAuthenticationDetails.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/TenantAwareAuthenticationDetails.java)**
    - Implements `Serializable`
    - Contains tenant information in authentication details
    - Enables multi-tenant authentication scenarios

13. **[PermissionService.java](hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/PermissionService.java)**
    - Manages user permissions
    - Handles authorization checks
    - Determines what operations users can perform

---

## 4. Security Integration & Filters

### Location: `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/`

#### Base Authentication Filters

1. **[AbstractControllerAuthenticationFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/AbstractControllerAuthenticationFilter.java)**
   - Base class for all controller authentication filters
   - Provides common authentication filter logic

2. **[PreAuthenticationFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/PreAuthenticationFilter.java)**
   - Marker interface for pre-authentication filters
   - Used by Spring Security filter chain

#### Controller-Level Authentication Filters

3. **[ControllerPreAuthenticateSecurityTokenFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/ControllerPreAuthenticateSecurityTokenFilter.java)**
   - Authenticates using security tokens
   - Handles target device authentication
   - Filters requests with token-based credentials

4. **[ControllerPreAuthenticatedGatewaySecurityTokenFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/ControllerPreAuthenticatedGatewaySecurityTokenFilter.java)**
   - Gateway-based token authentication
   - Handles gateway pre-authenticated requests

5. **[ControllerPreAuthenticatedSecurityHeaderFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/ControllerPreAuthenticatedSecurityHeaderFilter.java)**
   - Extracts authentication from HTTP headers
   - Handles header-based pre-authentication

6. **[ControllerPreAuthenticatedAnonymousFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/ControllerPreAuthenticatedAnonymousFilter.java)**
   - Implements `PreAuthenticationFilter`
   - Handles anonymous access scenarios

7. **[ControllerPreAuthenticatedAnonymousDownload.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/ControllerPreAuthenticatedAnonymousDownload.java)**
   - Anonymous download authentication
   - Allows unauthenticated file downloads under conditions

#### Authentication Providers

8. **[PreAuthTokenSourceTrustAuthenticationProvider.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/PreAuthTokenSourceTrustAuthenticationProvider.java)**
   - Implements Spring's `AuthenticationProvider`
   - Validates pre-authenticated tokens
   - Trusts the token source

#### Security Token & Authentication Details

9. **[DmfTenantSecurityToken.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/DmfTenantSecurityToken.java)**
   - Device Management Federation (DMF) tenant security token
   - Represents a tenant-specific security token

10. **[HeaderAuthentication.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/HeaderAuthentication.java)**
    - Authentication details from HTTP headers
    - Stores header-based authentication information

11. **[TenantAwareWebAuthenticationDetails.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/TenantAwareWebAuthenticationDetails.java)**
    - Web-based tenant-aware authentication details
    - Includes tenant context in web authentication

#### OIDC Authentication

### Location: `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/`

##### OIDC Core Components

1. **[OidcBearerTokenAuthenticationFilter.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/OidcBearerTokenAuthenticationFilter.java)**
   - Implements `UserAuthenticationFilter` and `Filter`
   - Validates OAuth2 bearer tokens
   - Extracts and validates JWT tokens from requests

2. **[JwtAuthoritiesExtractor.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/JwtAuthoritiesExtractor.java)**
   - Extracts user authorities from JWT tokens
   - Parses claims to determine user permissions

3. **[JwtAuthoritiesValidator.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/JwtAuthoritiesValidator.java)**
   - Validates JWT token structure and claims
   - Ensures token integrity

4. **[JwtAuthoritiesOidcUserService.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/JwtAuthoritiesOidcUserService.java)**
   - Extends Spring's `OidcUserService`
   - Custom OIDC user service with JWT authorities
   - Loads user information from OIDC provider

##### OIDC Success & Error Handlers

5. **[OidcAuthenticationSuccessHandler.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/OidcAuthenticationSuccessHandler.java)**
   - Extends `SavedRequestAwareAuthenticationSuccessHandler`
   - Handles successful OIDC authentication
   - Redirects to originally requested page or default

6. **[OidcRestAuthenticationEntryPoint.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/OidcRestAuthenticationEntryPoint.java)**
   - Implements Spring's `AuthenticationEntryPoint`
   - REST API entry point for OIDC authentication
   - Handles unauthenticated REST requests

##### OIDC Logout

7. **[OidcLogoutHandler.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/OidcLogoutHandler.java)**
   - Extends `SecurityContextLogoutHandler`
   - Handles OIDC logout operations

8. **[OidcLogoutSuccessHandler.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/OidcLogoutSuccessHandler.java)**
   - Extends `SimpleUrlLogoutSuccessHandler`
   - Post-logout redirect handling

##### OIDC Audit & Utilities

9. **[OidcUserAuditService.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/OidcUserAuditService.java)**
    - Audits OIDC user authentication events
    - Logs user login/logout activities

10. **[JwtUtil.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/util/JwtUtil.java)**
    - JWT token utility functions
    - Parses and validates JWT tokens
    - Extracts claims from tokens

##### OIDC Model Classes

### Location: `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/model/`

11. **[UserTenantAuthorities.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/model/UserTenantAuthorities.java)**
    - Represents user authorities per tenant
    - Maps tenant-to-authorities relationship

12. **[UserTenantRole.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/model/UserTenantRole.java)**
    - Represents user role within a tenant
    - Stores role information

##### OIDC Exception Handling

### Location: `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/exception/`

13. **[OAuth2AccessDeniedException.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/exception/OAuth2AccessDeniedException.java)**
    - Extends Spring's `AuthenticationException`
    - Thrown when OAuth2 authorization fails

14. **[ExceptionInfo.java](hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/exception/ExceptionInfo.java)**
    - Exception information model
    - Contains error details for clients

---

## 5. HTTP-Level Security Filters

### Location: `hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/`

1. **[AbstractHttpControllerAuthenticationFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/AbstractHttpControllerAuthenticationFilter.java)**
   - Base class for HTTP-level authentication filters
   - Provides common HTTP filtering logic

2. **[HttpControllerPreAuthenticateSecurityTokenFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/HttpControllerPreAuthenticateSecurityTokenFilter.java)**
   - HTTP security token pre-authentication filter
   - Processes security tokens from HTTP requests

3. **[HttpControllerPreAuthenticatedGatewaySecurityTokenFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/HttpControllerPreAuthenticatedGatewaySecurityTokenFilter.java)**
   - HTTP gateway token pre-authentication filter
   - Handles gateway-specific HTTP authentication

4. **[HttpControllerPreAuthenticatedSecurityHeaderFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/HttpControllerPreAuthenticatedSecurityHeaderFilter.java)**
   - HTTP header-based pre-authentication filter
   - Extracts security information from HTTP headers

5. **[HttpControllerPreAuthenticateAnonymousDownloadFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/HttpControllerPreAuthenticateAnonymousDownloadFilter.java)**
   - Anonymous download HTTP filter
   - Allows HTTP downloads without authentication

6. **[HttpDownloadAuthenticationFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/HttpDownloadAuthenticationFilter.java)**
   - Download-specific authentication filter
   - Handles download request authentication

7. **[ControllerTenantAwareAuthenticationDetailsSource.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/ControllerTenantAwareAuthenticationDetailsSource.java)**
   - Tenant-aware HTTP authentication details source
   - Provides tenant context for HTTP authentication

8. **[DosFilter.java](hawkbit-http-security/src/main/java/org/eclipse/hawkbit/security/DosFilter.java)**
   - Denial of Service (DoS) protection filter
   - Protects against DoS attacks

---

## 6. Configuration Files

### cosmos-mgmt-api Configuration Files

#### Location: `cosmos-mgmt-api/src/main/resources/`

1. **[application-int.properties](cosmos-mgmt-api/src/main/resources/application-int.properties)**
   - Integration environment configuration
   - Key settings:
     - `spring.security.user.name=admin`
     - `spring.security.user.password={noop}admin`
     - DDI security authentication settings:
       - Anonymous: disabled
       - Target token: enabled
       - Gateway token: disabled
     - DOS protection limits

2. **[application-local.properties](cosmos-mgmt-api/src/main/resources/application-local.properties)**
   - Local development environment configuration
   - OAuth2/OIDC Configuration:
     - Client ID: `MUWABNBDNBCKBAXEQPYRQNFCECFPKVWD`
     - Client Secret: (provided in file)
     - Scope: `openid,prd:aug,profile`
     - Provider Type: `custom`
     - Authorized Clients: Multiple client IDs configured
     - OIDC Endpoints:
       - Issuer URI: `https://idfed-preprod.mpsa.com:443`
       - Authorization URI: `https://idfed-preprod.mpsa.com:443/as/authorization.oauth2`
       - Token URI: `https://idfed-preprod.mpsa.com:443/as/token.oauth2`
       - User Info URI: `https://idfed-preprod.mpsa.com:443/idp/userinfo.openid`
       - JWK Set URI: `https://idfed-preprod.mpsa.com:443/pf/JWKS`
       - Logout URI: `https://idfed-preprod.mpsa.com/idp/startSLO.ping`

3. **[application-qa.properties](cosmos-mgmt-api/src/main/resources/application-qa.properties)**
   - Quality Assurance environment configuration
   - Security settings similar to integration

4. **[application.properties](cosmos-mgmt-api/src/main/resources/application.properties)**
   - Default/fallback configuration

---

## 7. Key Authentication Methods Supported

### Based on Configuration Analysis

1. **Token-Based Authentication**
   - Security tokens passed in headers
   - Pre-authenticated token validation

2. **OAuth2 / OpenID Connect (OIDC)**
   - JWT bearer token authentication
   - OIDC provider integration (idfed-preprod.mpsa.com)
   - Authorized client validation

3. **Header-Based Authentication**
   - Security credentials in HTTP headers
   - Custom header extraction

4. **Gateway Token Authentication**
   - Gateway-specific token handling
   - Can be disabled/enabled per environment

5. **Anonymous Access**
   - Controlled anonymous downloads
   - Disabled in production, enabled in development

6. **HTTP Basic Authentication**
   - Username/password via Spring Security
   - In-memory user: admin/{noop}admin

---

## 8. Key Security Concepts

### Multi-Tenancy
- Tenant-aware authentication at multiple levels:
  - `SecurityContextTenantAware`
  - `TenantUserPasswordAuthenticationToken`
  - `TenantAwareAuthenticationDetails`
  - JWT claims-based tenant resolution

### DoS Protection
- `DosFilter` in HTTP layer
- Configurable artifact size limits (1GB in prod)
- Storage limits (100GB in prod)

### Audit & Compliance
- `SpringSecurityAuditorAware` for action tracking
- `OidcUserAuditService` for OAuth2 audit logging
- Tracks authenticated user for all operations

### JWT Token Management
- `JwtUtil` for token parsing
- `JwtAuthoritiesExtractor` for claims extraction
- `JwtAuthoritiesValidator` for validation

### Authorization
- `PermissionService` for permission checks
- Role-based access control (RBAC)
- Tenant-scoped permissions

---

## 9. Security Filter Chain

The security filter chain processes requests in this order:

1. **DoS Protection**: `DosFilter`
2. **Pre-Authentication Filters**:
   - `HttpControllerPreAuthenticateSecurityTokenFilter` (token)
   - `HttpControllerPreAuthenticatedGatewaySecurityTokenFilter` (gateway)
   - `HttpControllerPreAuthenticatedSecurityHeaderFilter` (header)
   - `HttpControllerPreAuthenticateAnonymousDownloadFilter` (anonymous)
3. **JWT/OIDC Filters**:
   - `OidcBearerTokenAuthenticationFilter` (JWT validation)
4. **Post-Authentication**: Spring Security standard chain

---

## 10. Environment-Specific Security Settings

### Local Development
- Anonymous access: **ENABLED**
- Target token: **ENABLED**
- Gateway token: **ENABLED**
- User: admin / admin
- OIDC: Connected to preprod OIDC provider

### Integration/QA/Production
- Anonymous access: **DISABLED**
- Target token: **ENABLED**
- Gateway token: **DISABLED**
- User: admin (password via Spring Security)
- Security properties configured via environment variables

---

## 11. Root Security Flows

### OAuth2/OIDC Authentication Flow
1. User initiates login
2. `OidcAuthenticationSuccessHandler` redirects to OIDC provider
3. Provider returns authorization code
4. Backend exchanges code for tokens
5. JWT extracted and validated by `JwtAuthoritiesExtractor`
6. User authorities loaded via `JwtAuthoritiesOidcUserService`
7. User logged in with `OidcUserAuditService` for audit

### Token-Based API Authentication Flow
1. Client sends request with security token
2. `HttpControllerPreAuthenticateSecurityTokenFilter` extracts token
3. `PreAuthTokenSourceTrustAuthenticationProvider` validates token
4. `ControllerTenantAwareAuthenticationDetailsSource` adds tenant context
5. Request processed with authenticated context

---

## Summary Statistics

- **Total Security-Related Java Classes**: 113+ files
- **Configuration Files**: 6+ property files
- **Autoconfiguration Classes**: 6 main security configuration classes
- **Authentication Filters**: 15+ filter implementations
- **OIDC Components**: 10+ OIDC-specific classes
- **Management API Security Entry Point**: 1 main Start.java class
- **Supported Authentication Methods**: 5 primary methods
- **Multi-tenant Support**: Full tenant-aware security context throughout
