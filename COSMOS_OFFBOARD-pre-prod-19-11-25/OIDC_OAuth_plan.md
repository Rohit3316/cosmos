## Plan: Replace Remote OAuth with Local Keycloak for Bearer Token Access

TL;DR: Replace the external OIDC provider URLs in the local `application-local.properties` for the update server with local Keycloak endpoints, then configure Keycloak to issue bearer JWTs that match the app's expected `resource_access.<clientId>.roles` claim structure.


   - Local Keycloak and PostgreSQL run commands:
     - `docker run --network host -e KC_HTTP_PORT=9090 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -e KC_DB=postgres -e KC_DB_URL=jdbc:postgresql://127.0.0.1:5433/keycloak -e KC_DB_USERNAME=keycloak -e KC_DB_PASSWORD=password quay.io/keycloak/keycloak:latest start-dev`
     - `docker run --name postgres-keycloak -e POSTGRES_DB=keycloak -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=password -p 5433:5432 -d postgres:16`

**Steps**
1. Identify and update the local OIDC configuration in the relevant local properties file(s).
   - Primary file: `hawkbit-runtime/hawkbit-update-server/src/main/resources/application-local.properties`
   - Also inspect and align any sibling local property files in modules that can run the same service locally, such as `cosmos-mgmt-api/src/main/resources/application-local.properties` and `cosmos-ddi-api/src/main/resources/application-local.properties`.
   - Replace `spring.security.oauth2.client.provider.oidc.*` values that currently point to `https://idfed-preprod.mpsa.com:443` with local Keycloak endpoints.

   - Local Keycloak and PostgreSQL are already running via Docker, so the next step is binding app config to the local Keycloak realm and client.

2. Choose the local Keycloak realm and client configuration.
   - Create (or reuse) a Keycloak realm for local COSMOS development.
   - Create a new Keycloak client with its own `client-id` and `client-secret`.
   - Update `spring.security.oauth2.client.registration.oidc.client-id` and `spring.security.oauth2.client.registration.oidc.client-secret` in `application-local.properties` to match the new Keycloak client values.

   - Use the Keycloak admin console at `http://localhost:9090/admin/master/console/`.
   - Log in with `admin/admin`, create a realm such as `cosmos-local`, then create a confidential client in that realm.
   - Configure the client to support token issuance; if using direct access tokens for Postman, enable `Service Accounts` or `Client Credentials`.
   - Add a local test user and assign the required roles/client roles so the access token contains valid authorities.

3. Configure Keycloak endpoints for local use.
   - Use the Keycloak realm's OIDC endpoints for:
     - `issuer-uri`
     - `authorization-uri`
     - `token-uri`
     - `user-info-uri`
     - `jwk-set-uri`
     - `logout-uri`
   - Typical local Keycloak paths are:
     - `http://localhost:8080/realms/<realm>` for issuer
     - `http://localhost:8080/realms/<realm>/protocol/openid-connect/auth`
     - `http://localhost:8080/realms/<realm>/protocol/openid-connect/token`
     - `http://localhost:8080/realms/<realm>/protocol/openid-connect/userinfo`
     - `http://localhost:8080/realms/<realm>/protocol/openid-connect/certs`
     - `http://localhost:8080/realms/<realm>/protocol/openid-connect/logout`
   - Adjust the hostname/port to your local Keycloak instance.

4. Ensure the local token format is compatible with the application.
   - The application expects valid JWT verification via `jwk-set-uri` and `issuer-uri`.
   - `JwtAuthoritiesExtractor` requires the token to expose roles in one of these shapes:
     - `resource_access.<clientId>.roles` (preferred)
     - or a top-level `roles` claim
   - Configure Keycloak role mappings so the access token includes `resource_access[<client-id>].roles` for the configured client.
   - Ensure any realm or client roles required by COSMOS are mapped and included.

5. Confirm the security flow in the app.
   - `SecurityManagedConfiguration` loads `OidcBearerTokenAuthenticationFilter` when OIDC is enabled.
   - It uses the first available `ClientRegistration` and the configured JWKS endpoint to validate bearer tokens.
   - Postman must send `Authorization: Bearer <token>` for `/management/v1/**` requests.
   - Basic auth is not used when the OIDC bearer filter is active, so local bearer tokens are mandatory.

6. Configure Keycloak users/roles for management access.
   - Create a Keycloak user or service account that can obtain tokens for local testing.
   - Assign roles and client role mappings required to satisfy the app's authorization checks for `/management/**`.
   - If the app uses custom tenant-role formatting, verify the JWT claim values match the parser expectations in `JwtAuthoritiesExtractor`.

7. Test with Postman.
   - Request an access token from the Keycloak token endpoint using the configured client.
   - In Postman, send `Authorization: Bearer <access_token>` to a local management API endpoint like `http://localhost:8083/management/v1/...`.
   - Validate 200 or expected management API response instead of 401.

8. Verify and harden.
   - Confirm the local app accepts the token and resolves roles from JWT claims.
   - If using self-signed HTTPS on Keycloak, ensure the local JVM trusts the certificate or use HTTP for dev.
   - Keep external IdP references out of local profile config to avoid accidental remote calls.

**Relevant files**
- `hawkbit-runtime/hawkbit-update-server/src/main/resources/application-local.properties`
- `cosmos-mgmt-api/src/main/resources/application-local.properties`
- `cosmos-ddi-api/src/main/resources/application-local.properties`
- `hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/SecurityManagedConfiguration.java`
- `hawkbit-security-integration/src/main/java/org/eclipse/hawkbit/security/oidc/authentication/JwtAuthoritiesExtractor.java`
- `hawkbit-autoconfigure/src/main/java/org/eclipse/hawkbit/autoconfigure/security/OidcUserManagementAutoConfiguration.java`

**Verification**
1. Update local properties and restart the local server under the `local` profile.
2. Use Postman to obtain a bearer token from Keycloak and call `http://localhost:8083/management/v1/...` with `Authorization: Bearer <token>`.
3. Confirm the server accepts the token and returns a management API response rather than 401.
4. Optionally, inspect the token claims to verify `resource_access.<clientId>.roles` and issuer/JWKS alignment.

**Decisions**
- Local Keycloak will replace the remote provider only for the local development profile.
- The app will continue to use OIDC bearer tokens, not Basic Authentication, on `/management/**`.
- Compatibility depends on Keycloak issuing JWTs that conform to the app's role claim expectations.

**Further considerations**
1. If the application needs a second client for Postman, create it in Keycloak and add it to `spring.security.oauth2.client.provider.oidc.authorized-clients`.
2. If you want to avoid editing production-style files, put the Keycloak endpoints in a local overlay or env-vars rather than the shared config.
3. If Keycloak is not yet available, start with a minimal Keycloak realm and one client so the token flow can be validated quickly.
